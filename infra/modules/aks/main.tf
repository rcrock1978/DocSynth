resource "azurerm_kubernetes_cluster" "main" {
  name                = "${var.env}-docsynth-aks"
  location            = var.location
  resource_group_name = var.resource_group_name
  dns_prefix          = "${var.env}-docsynth"
  kubernetes_version  = "1.30"

  default_node_pool {
    name                = "system"
    node_count          = 3
    vm_size             = "Standard_D4s_v5"
    zones               = ["1", "2", "3"]
    enable_auto_scaling = true
    min_count           = 3
    max_count           = 10
  }

  identity {
    type = "SystemAssigned"
  }

  oidc_issuer_enabled       = true
  workload_identity_enabled = true

  network_profile {
    network_plugin    = "azure"
    network_policy    = "cilium"
    load_balancer_sku = "standard"
  }

  tags = var.tags
}
