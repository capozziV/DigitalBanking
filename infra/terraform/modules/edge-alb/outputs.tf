output "listener_arn" {
  description = "Listener HTTP do ALB; destino da integracao do VPC Link da borda."
  value       = aws_lb_listener.http.arn
}

output "target_group_arns" {
  description = "Mapa servico -> ARN do target group (targetGroupArn do chart Helm)."
  value       = { for service, tg in aws_lb_target_group.service : service => tg.arn }
}

output "service_ports" {
  description = "Mapa servico -> porta do container."
  value       = local.services
}

output "alb_security_group_id" {
  value = aws_security_group.alb.id
}

output "vpc_link_security_group_id" {
  value = aws_security_group.vpc_link.id
}

output "alb_dns_name" {
  value = aws_lb.this.dns_name
}
