terraform {
  /*
  backend "s3" {
    bucket         = "terraform-state-bucket"
    key            = "terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "terraform-state-lock"
  }
  */

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 3.0"
    }
  }
}

variable "app_prefix" {
  default = "ev-outlet-app"
}

provider "aws" {
  region = "eu-west-1"
}

resource "aws_kinesis_stream" "outlet-events-kinesis-stream" {
  name        = "${var.app_prefix}.outlet-events.stream"
  shard_count = 4
}


resource "aws_kinesis_stream" "dead-letters-kinesis-stream" {
  name         = "${var.app_prefix}.dead-letters.stream"
  shard_count = 1
}


resource "aws_dynamodb_table" "app-backend-checkpoints-dynamodb-table" {
  name           = "${var.app_prefix}.outlet-events-checkpoints.app-backend.table"
  billing_mode   = "PROVISIONED"
  read_capacity  = 4
  write_capacity = 4
  hash_key       = "leaseKey"

  attribute {
    name = "leaseKey"
    type = "S"
  }
}

resource "aws_dynamodb_table" "outlet-backend-checkpoints-dynamodb-table" {
  name           = "${var.app_prefix}.outlet-events-checkpoints.outlet-backend.table"
  billing_mode   = "PROVISIONED"
  read_capacity  = 4
  write_capacity = 4
  hash_key       = "leaseKey"

  attribute {
    name = "leaseKey"
    type = "S"
  }
}

resource "aws_dynamodb_table" "customer-dynamodb-table" {
  name           = "${var.app_prefix}.customer.table"
  billing_mode   = "PROVISIONED"
  read_capacity  = 100
  write_capacity = 100
  hash_key       = "customerId"

  attribute {
    name = "customerId"
    type = "S"
  }

  attribute {
    name = "rfidTag"
    type = "S"
  }

  global_secondary_index {
    name               = "${var.app_prefix}.customer-rfidTag.index"
    hash_key           = "rfidTag"
    write_capacity     = 100
    read_capacity      = 100
    projection_type    = "INCLUDE"
    non_key_attributes = ["customerId"]
  }

  lifecycle {
    ignore_changes = [write_capacity, read_capacity]
  }
}

resource "aws_dynamodb_table" "charger-outlet-dynamodb-table" {
  name           = "${var.app_prefix}.charger-outlet.table"
  billing_mode   = "PROVISIONED"
  read_capacity  = 100
  write_capacity = 100
  hash_key       = "outletId"

  attribute {
    name = "outletId"
    type = "S"
  }

  lifecycle {
    ignore_changes = [write_capacity, read_capacity]
  }
}

resource "aws_dynamodb_table" "charging-session-dynamodb-table" {
  name           = "${var.app_prefix}.charging-session.table"
  billing_mode   = "PROVISIONED"
  read_capacity  = 100
  write_capacity = 100
  hash_key       = "sessionId"

  attribute {
    name = "sessionId"
    type = "S"
  }

  attribute {
    name = "customerId"
    type = "S"
  }

  global_secondary_index {
    name               = "${var.app_prefix}.charging-session-customerId.index"
    hash_key           = "customerId"
    write_capacity     = 100
    read_capacity      = 100
    projection_type    = "ALL"
  }

  lifecycle {
    ignore_changes = [write_capacity, read_capacity]
  }
}
