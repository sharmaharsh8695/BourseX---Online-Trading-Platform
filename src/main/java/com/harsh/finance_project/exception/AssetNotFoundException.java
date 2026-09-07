package com.harsh.finance_project.exception;

public class AssetNotFoundException extends RuntimeException {
    public AssetNotFoundException(Long assetId) {
        super("Asset not found with id: " + assetId);
    }
}
