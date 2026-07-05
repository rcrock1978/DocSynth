resource "azurerm_key_vault" "main" {
  name                = "${var.env}-docsynth-kv"
  location            = var.location
  resource_group_name = var.resource_group_name
  tenant_id           = var.aad_tenant_id
  sku_name            = "premium"
  soft_delete_retention_days = 90
  enable_rbac_authorization  = true
  purge_protection_enabled  = true
  tags                = var.tags
}

resource "azurerm_key_vault_access_policy" "backend" {
  key_vault_id = azurerm_key_vault.main.id
  tenant_id    = var.aad_tenant_id
  object_id    = var.backend_managed_identity_object_id

  secret_permissions = [
    "Get", "List", "Set", "Delete", "Recover", "Backup", "Restore",
  ]
}
