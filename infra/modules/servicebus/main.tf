resource "azurerm_servicebus_namespace" "main" {
  name                = "${var.env}-docsynth-sb"
  location            = var.location
  resource_group_name = var.resource_group_name
  sku                 = "Premium"
  capacity            = 1
  tags                = var.tags
}

# Geo-disaster recovery pairing with secondary region.
resource "azurerm_servicebus_namespace_disaster_recovery_config" "main" {
  name                 = "docsynth-dr-${var.env}"
  resource_group_name  = var.resource_group_name
  namespace_name       = azurerm_servicebus_namespace.main.name
  partner_namespace_id = var.partner_servicebus_namespace_id
  depends_on           = [var.partner_servicebus_namespace_id]
}

resource "azurerm_servicebus_topic" "events" {
  for_each = toset([
    "spec.parsed",
    "drift.detect",
    "docset.build",
    "docset.publish",
    "docset.transition",
    "notification.dispatch",
  ])

  name         = each.value
  namespace_id = azurerm_servicebus_namespace.main.id

  enable_partitioning = true
  max_size_in_megabytes = 5120
}

resource "azurerm_servicebus_subscription" "consumers" {
  for_each = {
    "docset.build.spec-parsed"  = { topic = "spec.parsed",    name = "docset-build" }
    "drift.detect.spec-parsed"   = { topic = "spec.parsed",    name = "drift-detect" }
    "notification.drift.detect"  = { topic = "drift.detect",   name = "notification-dispatch" }
  }

  name               = "${each.value.topic}.${each.value.name}"
  topic_id           = azurerm_servicebus_topic.events[each.value.topic].id
  max_delivery_count = 5
  lock_duration      = "PT1M"

  # Dead-letter on max retries
  dead_lettering_on_message_expiration = true
}
