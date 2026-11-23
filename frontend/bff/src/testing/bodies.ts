/** Bodies exactly as the services answer them (Problem Details of each `ProblemDetailsExceptionHandler`). */
export const URLS = {
  accounts: 'http://localhost:8081',
  transfers: 'http://localhost:8082',
  notification: 'http://localhost:8083',
  statement: 'http://localhost:8084',
  customer: 'http://localhost:8085',
};

export const IDS = {
  customer: '11111111-1111-4111-8111-111111111111',
  account: '22222222-2222-4222-8222-222222222222',
  otherCustomer: '33333333-3333-4333-8333-333333333333',
  // With letters, so that its upper case is another string.
  otherAccount: '44444444-4444-4444-8444-44444444abcd',
  transfer: '55555555-5555-4555-8555-555555555555',
  notification: '66666666-6666-4666-8666-666666666666',
};

export function serviceProblem(
  status: number,
  slug: string,
  title: string,
  detail: string,
  instance: string,
  properties: Record<string, unknown> = {},
) {
  return {
    status,
    body: { type: `/problems/${slug}`, title, status, detail, instance, ...properties },
  };
}
