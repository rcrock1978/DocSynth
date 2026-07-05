terraform {
  required_version = ">= 1.9.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
  }
}

provider "azurerm" {
  features {}
}

# Module declarations live under infra/modules/ and are wired per env in
# infra/envs/{dev,staging,prod}. This file is the root entry point only.
