# AWS Load Balancer Controller so para TargetGroupBinding: registra os IPs dos pods nos target groups
# do Terraform. Nao cria ALB, NLB nem target group: o papel nao tem permissao para isso, nao existe
# IngressClass, e o controller de Service e o Gateway API estao desligados. Assim o destroy do Terraform
# nao deixa load balancer orfao.

locals {
  lb_controller_service_account = "aws-load-balancer-controller"
}

data "aws_iam_policy_document" "pod_identity_assume" {
  statement {
    actions = ["sts:AssumeRole", "sts:TagSession"]

    principals {
      type        = "Service"
      identifiers = ["pods.eks.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "lb_controller" {
  statement {
    sid = "Describe"
    actions = [
      "ec2:DescribeAvailabilityZones",
      "ec2:DescribeInstances",
      "ec2:DescribeNetworkInterfaces",
      "ec2:DescribeSecurityGroups",
      "ec2:DescribeSubnets",
      "ec2:DescribeVpcs",
      "elasticloadbalancing:DescribeListeners",
      "elasticloadbalancing:DescribeLoadBalancers",
      "elasticloadbalancing:DescribeTags",
      "elasticloadbalancing:DescribeTargetGroupAttributes",
      "elasticloadbalancing:DescribeTargetGroups",
      "elasticloadbalancing:DescribeTargetHealth",
    ]
    resources = ["*"]
  }

  statement {
    sid       = "RegisterPods"
    actions   = ["elasticloadbalancing:RegisterTargets", "elasticloadbalancing:DeregisterTargets"]
    resources = var.target_group_arns
  }
}

resource "aws_iam_role" "lb_controller" {
  name               = "${var.name}-lb-controller"
  assume_role_policy = data.aws_iam_policy_document.pod_identity_assume.json
}

resource "aws_iam_role_policy" "lb_controller" {
  name   = "target-group-binding"
  role   = aws_iam_role.lb_controller.id
  policy = data.aws_iam_policy_document.lb_controller.json
}

resource "aws_eks_pod_identity_association" "lb_controller" {
  cluster_name    = aws_eks_cluster.this.name
  namespace       = "kube-system"
  service_account = local.lb_controller_service_account
  role_arn        = aws_iam_role.lb_controller.arn

  depends_on = [aws_eks_addon.after_nodes]
}

resource "helm_release" "lb_controller" {
  name       = "aws-load-balancer-controller"
  repository = "https://aws.github.io/eks-charts"
  chart      = "aws-load-balancer-controller"
  version    = var.lb_controller_chart_version
  namespace  = "kube-system"
  wait       = true
  timeout    = 600

  values = [yamlencode({
    clusterName  = aws_eks_cluster.this.name
    region       = var.region
    vpcId        = var.vpc_id
    replicaCount = 1

    serviceAccount = {
      create = true
      name   = local.lb_controller_service_account
    }

    createIngressClassResource  = false
    enableServiceMutatorWebhook = false
    enableShield                = false
    enableWaf                   = false
    enableWafv2                 = false

    controllerConfig = {
      featureGates = {
        EnableServiceController = false
        ALBGatewayAPI           = false
        NLBGatewayAPI           = false
      }
    }
  })]

  depends_on = [
    aws_eks_pod_identity_association.lb_controller,
    aws_iam_role_policy.lb_controller,
  ]
}
