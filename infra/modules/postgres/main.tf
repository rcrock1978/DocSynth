resource "azurerm_postgresql_flexible_server" "main" {
  name                          = "${var.env}-docsynth-postgres"
  resource_group_name           = var.resource_group_name
  location                      = var.location
  version                       = "16"
  delegated_subnet_id           = var.postgres_subnet_id
  private_dns_zone_id           = var.postgres_private_dns_zone_id
  public_network_access_enabled = false
  zone                          = "1"
  high_availability {
    mode                      = "ZoneRedundant"
    standby_availability_zone = "2"
  }
  backup_retention_days        = 35
  geo_redundant_backup_enabled = true
  administrator_login          = var.postgres_admin_login
  administrator_password       = var.postgres_admin_password
  sku_name                     = "GP_Standard_D4s_v3"
  storage_mb                   = 524288
  storage_tier                 = "P30"

  tags = var.tags
}

resource "azurerm_postgresql_flexible_server_database" "docsynth" {
  name      = "docsynth"
  server_id = azurerm_postgresql_flexible_server.main.id
  collation = "en_US.utf8"
}

resource "azurerm_postgresql_flexible_server_configuration" "extensions" {
  for_each  = toset(["pgcrypto", "vector", "pg_stat_statements"])
  name      = "azure.extensions"
  server_id = azurerm_postgresql_flexible_server.main.id
  value     = join(",", concat(
    split(",", try(coalesce(try(azurerm_postgresql_flexible_server_configuration_previous[each.key].value, null), ""))),
    [each.key],
  ))
}

# Point-in-time recovery (PITR) is enabled by default with backup_retention_days >= 7.
# Cross-region read replica for geo-DR is provisioned per-env as needed.

resource "azurerm_postgresql_flexible_server_firewall_rule" "allow_aks" {
  name             = "allow-aks-subnet"
  server_id        = azurerm_postgresql_flexible_server.main.id
  start_ip_address = "10.0.0.0"
  end_ip_address   = "10.255.255.255"
}

output "postgres_fqdn" {
  value = azurerm_postgresql_flexible_server.main.fqdn
}
