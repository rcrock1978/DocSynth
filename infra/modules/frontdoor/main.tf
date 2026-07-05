resource "azurerm_cdn_frontdoor_profile" "main" {
  name                = "${var.env}-docsynth-fd"
  resource_group_name = var.resource_group_name
  location            = "global"
  sku_name            = "Premium_AzureFrontDoor"
  tags                = var.tags
}

resource "azurerm_cdn_frontdoor_endpoint" "docs" {
  name                     = "${var.env}-docsynth-docs"
  cdn_frontdoor_profile_id = azurerm_cdn_frontdoor_profile.main.id
}

resource "azurerm_cdn_frontdoor_origin_group" "backend" {
  name                     = "backend"
  cdn_frontdoor_profile_id = azurerm_cdn_frontdoor_profile.main.id

  load_balancing {
    sample_size                        = 4
    successful_samples_required        = 3
    additional_latency_in_milliseconds = 50
  }

  health_probe {
    path                = "/actuator/health/liveness"
    request_type        = "HEAD"
    probe_method        = "HEAD"
    interval_in_seconds = 30
  }
}

resource "azurerm_cdn_frontdoor_origin" "primary" {
  name                          = "primary-aks"
  cdn_frontdoor_origin_group_id = azurerm_cdn_frontdoor_origin_group.backend.id
  enabled                       = true

  host_name          = var.primary_aks_ingress_fqdn
  http_port          = 80
  https_port         = 443
  origin_host_header = var.primary_aks_ingress_fqdn
  priority           = 1
  weight             = 1000
}

resource "azurerm_cdn_frontdoor_origin" "secondary" {
  name                          = "secondary-aks"
  cdn_frontdoor_origin_group_id = azurerm_cdn_frontdoor_origin_group.backend.id
  enabled                       = true

  host_name          = var.secondary_aks_ingress_fqdn
  http_port          = 80
  https_port         = 443
  origin_host_header = var.secondary_aks_ingress_fqdn
  priority           = 2
  weight             = 1000
}
