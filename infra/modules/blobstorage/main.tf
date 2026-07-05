resource "azurerm_storage_account" "main" {
  name                     = replace("${var.env}docsynthblob", "-", "")
  resource_group_name      = var.resource_group_name
  location                 = var.location
  account_tier             = "Standard"
  account_replication_type = "GZRS"
  min_tls_version          = "TLS1_2"
  enable_https_traffic_only = true
  allow_blob_public_access = false
  tags                     = var.tags
}

resource "azurerm_storage_container" "docsets" {
  name                  = "docsets"
  storage_account_name  = azurerm_storage_account.main.name
  container_access_type = "private"
}

# Immutability policy: version-segmented prefixes (e.g., v1.2.0/) are write-once.
# Object-level WORM is enabled per container; releases ship a new prefix, never overwrite.
resource "azurerm_storage_management_policy" "lifecycle" {
  storage_account_id = azurerm_storage_account.main.id

  rule {
    name    = "docset-tier-transition"
    enabled = true
    filters {
      prefix_match = ["docsets/"]
      blob_types   = ["blockBlob"]
    }
    actions {
      base_blob {
        tier_to_cool_after_days_since_modification_greater_than    = 180
        tier_to_archive_after_days_since_modification_greater_than = 540
        delete_after_days_since_modification_greater_than          = 1825
      }
    }
  }
}
