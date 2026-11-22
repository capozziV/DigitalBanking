#!/usr/bin/env python3
"""Tests of migrate_from_localstack.py against fake databases and a fake docker CLI (no Docker, no network).

Usage: python3 -B infra/local/dynamodb/migrate_from_localstack_test.py
"""
import copy
import unittest

import migrate_from_localstack as migration

KEYS = [{"AttributeName": "accountId", "KeyType": "HASH"}, {"AttributeName": "sequence", "KeyType": "RANGE"}]
TYPES = [{"AttributeName": "accountId", "AttributeType": "S"}, {"AttributeName": "sequence", "AttributeType": "N"}]


def account(account_id="a"):
    return {
        "accountId": {"S": account_id},
        "sequence": {"N": "0"},
        "itemType": {"S": "ACCOUNT"},
        "customerId": {"S": "customer"},
    }


def entry(sequence, account_id="a", amount="100"):
    return {
        "accountId": {"S": account_id},
        "sequence": {"N": str(sequence)},
        "itemType": {"S": "ENTRY"},
        "amount": {"N": amount},
        "description": {"S": "any attribute, known or not"},
        "nested": {"M": {"flag": {"BOOL": True}}},
    }


class FakeDynamoDb:
    """Scan pages have two items, so a copy that reads one page only is caught."""

    def __init__(self, items=(), table=True, keys=None):
        self.items = {(i["accountId"]["S"], i["sequence"]["N"]): copy.deepcopy(i) for i in items}
        self.table = table
        self.keys = keys or KEYS
        self.operations = []
        self.on_put = lambda item: item

    def call(self, operation, payload):
        self.operations.append(operation)
        if not self.table:
            raise migration.MigrationError(f"{operation}: ResourceNotFoundException")
        if operation == "DescribeTable":
            return {"Table": {"KeySchema": self.keys, "AttributeDefinitions": TYPES}}
        if operation == "Scan":
            assert payload["ConsistentRead"] is True
            start = payload.get("ExclusiveStartKey", {}).get("offset", 0)
            page = {"Items": copy.deepcopy([self.items[k] for k in sorted(self.items)][start:start + 2])}
            if start + 2 < len(self.items):
                page["LastEvaluatedKey"] = {"offset": start + 2}
            return page
        if operation == "PutItem":
            assert payload["ConditionExpression"] == "attribute_not_exists(accountId)"
            item = self.on_put(copy.deepcopy(payload["Item"]))
            if item is not None:
                self.items[(item["accountId"]["S"], item["sequence"]["N"])] = item
            return {}
        raise AssertionError(f"unexpected operation {operation}")


class MigrateTest(unittest.TestCase):
    def setUp(self):
        self.origin = [account(), entry(1), entry(2), account("b"), entry(1, "b")]
        self.source = FakeDynamoDb(self.origin)
        self.target = FakeDynamoDb()

    def test_copies_entries_and_accounts_with_every_attribute_across_pages(self):
        self.assertEqual(migration.migrate(self.source, self.target), (5, 5))
        self.assertEqual(self.target.items, FakeDynamoDb(self.origin).items)

    def test_never_writes_to_or_deletes_from_the_source(self):
        migration.migrate(self.source, self.target)
        self.assertEqual(set(self.source.operations), {"DescribeTable", "Scan"})

    def test_a_second_run_copies_nothing(self):
        migration.migrate(self.source, self.target)
        self.target.operations.clear()
        self.assertEqual(migration.migrate(self.source, self.target), (5, 0))
        self.assertNotIn("PutItem", self.target.operations)

    def test_finishes_an_interrupted_copy(self):
        self.target = FakeDynamoDb(self.origin[:2])
        self.assertEqual(migration.migrate(self.source, self.target), (5, 3))

    def test_the_same_key_with_other_content_fails_before_any_write(self):
        self.target = FakeDynamoDb([entry(1, amount="999")])
        with self.assertRaisesRegex(migration.MigrationError, "content"):
            migration.migrate(self.source, self.target)
        self.assertNotIn("PutItem", self.target.operations)

    def test_an_item_only_in_the_target_fails_before_any_write(self):
        self.target = FakeDynamoDb([entry(9)])
        with self.assertRaisesRegex(migration.MigrationError, "only in the target"):
            migration.migrate(self.source, self.target)
        self.assertNotIn("PutItem", self.target.operations)

    def test_a_different_count_after_the_copy_fails(self):
        self.target.on_put = lambda item: None if item["sequence"]["N"] == "2" else item
        with self.assertRaisesRegex(migration.MigrationError, "count"):
            migration.migrate(self.source, self.target)

    def test_different_content_after_the_copy_fails(self):
        self.target.on_put = lambda item: {name: value for name, value in item.items() if name != "description"}
        with self.assertRaisesRegex(migration.MigrationError, "content"):
            migration.migrate(self.source, self.target)

    def test_a_source_that_changes_during_the_copy_fails(self):
        def write_to_source(item):
            self.source.items[("c", "0")] = account("c")
            return item

        self.target.on_put = write_to_source
        with self.assertRaisesRegex(migration.MigrationError, "source changed"):
            migration.migrate(self.source, self.target)

    def test_a_missing_source_table_is_never_taken_for_an_empty_one(self):
        self.source.table = False
        with self.assertRaisesRegex(migration.MigrationError, "ResourceNotFoundException"):
            migration.migrate(self.source, self.target)
        self.assertNotIn("PutItem", self.target.operations)

    def test_another_key_schema_fails_before_any_write(self):
        self.target.keys = KEYS[:1]
        with self.assertRaisesRegex(migration.MigrationError, "schema"):
            migration.migrate(self.source, self.target)
        self.assertNotIn("PutItem", self.target.operations)


class FakeDocker:
    def __init__(self, statement=""):
        self.statement = statement
        self.projects = set()

    def __call__(self, *args):
        if args[0] == "port":
            return {"ls": "0.0.0.0:14566\n[::]:14566", "ddb": "127.0.0.1:18000"}[args[1]]
        labels = dict(a.removeprefix("label=com.docker.compose.").split("=", 1) for a in args if a.startswith("label="))
        self.projects.add(labels["project"])
        if labels["project"] != "mine":
            return ""
        return {"localstack": "ls", "dynamodb": "ddb", "statement": self.statement}[labels["service"]]


class CheckStackTest(unittest.TestCase):
    source = "http://127.0.0.1:14566"
    target = "http://127.0.0.1:18000"

    def test_accepts_the_endpoints_of_the_given_project(self):
        docker = FakeDocker()
        migration.check_stack("mine", self.source, self.target, docker)
        self.assertEqual(docker.projects, {"mine"})

    def test_refuses_the_port_of_another_stack(self):
        with self.assertRaisesRegex(migration.MigrationError, "localstack"):
            migration.check_stack("mine", "http://127.0.0.1:4566", self.target, FakeDocker())

    def test_refuses_swapped_endpoints(self):
        with self.assertRaisesRegex(migration.MigrationError, "localstack"):
            migration.check_stack("mine", self.target, self.source, FakeDocker())

    def test_refuses_a_project_without_the_containers(self):
        with self.assertRaisesRegex(migration.MigrationError, "other"):
            migration.check_stack("other", self.source, self.target, FakeDocker())

    def test_refuses_a_host_that_is_not_this_machine(self):
        with self.assertRaisesRegex(migration.MigrationError, "loopback"):
            migration.check_stack("mine", "http://example.test:14566", self.target, FakeDocker())

    def test_refuses_a_running_statement(self):
        with self.assertRaisesRegex(migration.MigrationError, "stop the statement"):
            migration.check_stack("mine", self.source, self.target, FakeDocker(statement="st"))


if __name__ == "__main__":
    unittest.main()
