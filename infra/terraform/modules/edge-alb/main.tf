locals {
  routing  = jsondecode(file("${path.module}/routes.json"))
  services = local.routing.services
  rules    = { for rule in local.routing.rules : tostring(rule.priority) => rule }
}

# VPC Link da borda -> ALB: so o security group do VPC Link entra no ALB, na porta 80.
resource "aws_security_group" "vpc_link" {
  name        = "${var.name}-vpc-link"
  description = "ENIs do VPC Link do API Gateway"
  vpc_id      = var.vpc_id
  tags        = { Name = "${var.name}-vpc-link" }
}

resource "aws_security_group" "alb" {
  name        = "${var.name}-alb"
  description = "ALB interno da borda"
  vpc_id      = var.vpc_id
  tags        = { Name = "${var.name}-alb" }
}

resource "aws_vpc_security_group_egress_rule" "vpc_link_to_alb" {
  security_group_id            = aws_security_group.vpc_link.id
  referenced_security_group_id = aws_security_group.alb.id
  ip_protocol                  = "tcp"
  from_port                    = 80
  to_port                      = 80
}

resource "aws_vpc_security_group_ingress_rule" "alb_from_vpc_link" {
  security_group_id            = aws_security_group.alb.id
  referenced_security_group_id = aws_security_group.vpc_link.id
  ip_protocol                  = "tcp"
  from_port                    = 80
  to_port                      = 80
}

# O ALB e do Terraform, nunca do AWS Load Balancer Controller: o destroy apaga o ALB, as ENIs dele
# e os target groups. O controller so registra os IPs dos pods (TargetGroupBinding).
resource "aws_lb" "this" {
  name                       = "${var.name}-edge"
  internal                   = true
  load_balancer_type         = "application"
  subnets                    = var.subnet_ids
  security_groups            = [aws_security_group.alb.id]
  drop_invalid_header_fields = true
  enable_deletion_protection = false # ambiente efemero, destruido por terraform destroy
}

# Um target group por servico, alvo por IP (pod na VPC CNI). O TargetGroupBinding do chart aponta
# para o ARN de cada um; o chart nao usa `networking`, quem abre a porta nos nos e o Terraform.
resource "aws_lb_target_group" "service" {
  for_each = local.services

  name                 = "${var.name}-${each.key}"
  port                 = each.value
  protocol             = "HTTP"
  target_type          = "ip"
  vpc_id               = var.vpc_id
  deregistration_delay = 15

  health_check {
    path                = local.routing.health[each.key]
    port                = "traffic-port"
    protocol            = "HTTP"
    matcher             = "200"
    interval            = 15
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

# Caminho sem regra: 404, como o nginx local.
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "fixed-response"

    fixed_response {
      content_type = "text/plain"
      status_code  = "404"
      message_body = "not found"
    }
  }
}

resource "aws_lb_listener_rule" "healthz" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 1

  action {
    type = "fixed-response"

    fixed_response {
      content_type = "text/plain"
      status_code  = "200"
      message_body = "ok"
    }
  }

  condition {
    path_pattern {
      values = ["/healthz"]
    }
  }
}

resource "aws_lb_listener_rule" "service" {
  for_each = local.rules

  listener_arn = aws_lb_listener.http.arn
  priority     = tonumber(each.key)

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.service[each.value.service].arn
  }

  condition {
    path_pattern {
      values       = try(each.value.paths, null)
      regex_values = try(each.value.regex, null)
    }
  }
}
