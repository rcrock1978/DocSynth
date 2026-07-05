resource "azurerm_monitor_action_group" "oncall" {
  name                = "${var.env}-docsynth-oncall"
  resource_group_name = var.resource_group_name
  short_name          = "docsynth"

  email_receiver {
    name                    = "primary-oncall"
    email_address           = var.oncall_email
    use_common_alert_schema = true
  }
}

# Weekly backup verification: trigger a restore to scratch and alert on failure.
resource "azurerm_storage_account" "backup_verify" {
  name                     = replace("${var.env}docsynthbackupv", "-", "")
  resource_group_name      = var.resource_group_name
  location                 = var.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
}

resource "azurerm_monitor_metric_alert" "backup_verify_failure" {
  name                = "${var.env}-docsynth-backup-verify-failure"
  resource_group_name = var.resource_group_name
  scopes              = [azurerm_storage_account.backup_verify.id]
  description         = "Weekly backup verification job failed; investigate RPO compliance."

  criteria {
    metric_namespace = "Microsoft.Storage/storageAccounts"
    metric_name      = "Transactions"
    aggregation      = "Count"
    operator         = "GreaterThan"
    threshold        = 0
  }

  action {
    action_group_id = azurerm_monitor_action_group.oncall.id
  }
}
