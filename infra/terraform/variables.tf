variable "aws_region" {
  description = "AWS region onde a infraestrutura será criada."
  type        = string
  default     = "us-east-2"
}

variable "project_name" {
  description = "Nome base do projeto."
  type        = string
  default     = "nitra-ai"
}

variable "environment" {
  description = "Nome do ambiente."
  type        = string
  default     = "prod"
}

variable "instance_type" {
  description = "Tipo de instância EC2. O padrão usa Graviton e é um bom equilíbrio entre custo e memória para app JVM pequena."
  type        = string
  default     = "t4g.micro"
}

variable "service_port" {
  description = "Porta pública da aplicação."
  type        = number
  default     = 8080
}

variable "allowed_ingress_cidrs" {
  description = "CIDRs autorizados a acessar a aplicação."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "artifact_retention_days" {
  description = "Dias para expirar artefatos antigos no bucket de deploy."
  type        = number
  default     = 30
}

variable "root_volume_size" {
  description = "Tamanho do volume raiz em GB."
  type        = number
  default     = 12
}
