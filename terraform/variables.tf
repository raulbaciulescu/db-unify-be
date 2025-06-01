variable "subscription_id" {
  description = "Azure Subscription ID"
  type        = string
}

variable "location" {
  default = "westeurope"
}

variable "resource_group_name" {
  default = "rg-db-unify"
}

variable "aks_cluster_name" {
  default = "aks-db-unify"
}

variable "acr_name" {
  default = "acrdbunify"
}

variable "node_count" {
  default = 1
}

variable "node_vm_size" {
  default = "Standard_B2s"
}

variable "postgres_admin_user" {
  type    = string
  default = "pgadmin"
}

variable "postgres_admin_password" {
  type      = string
  sensitive = true
}

variable "postgres_db_name" {
  type    = string
  default = "db-unify-boostrap-db"
}
