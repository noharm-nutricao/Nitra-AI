output "project_name" {
  description = "Nome base do projeto."
  value       = var.project_name
}

output "environment" {
  description = "Nome do ambiente."
  value       = var.environment
}

output "artifact_bucket_name" {
  description = "Bucket S3 onde os artefatos de deploy são publicados."
  value       = aws_s3_bucket.artifacts.bucket
}

output "instance_id" {
  description = "ID da instância EC2 da aplicação."
  value       = aws_instance.app.id
}

output "instance_public_ip" {
  description = "IP público da aplicação."
  value       = aws_instance.app.public_ip
}

output "instance_public_dns" {
  description = "DNS público da aplicação."
  value       = aws_instance.app.public_dns
}

output "service_port" {
  description = "Porta da aplicação."
  value       = var.service_port
}

output "service_url" {
  description = "URL base para acesso público."
  value       = "http://${aws_instance.app.public_dns}:${var.service_port}"
}

output "deploy_script_path" {
  description = "Caminho do script de deploy presente na instância."
  value       = "/usr/local/bin/deploy-${var.project_name}.sh"
}
