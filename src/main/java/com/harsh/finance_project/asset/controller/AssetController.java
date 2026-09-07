package com.harsh.finance_project.asset.controller;

import com.harsh.finance_project.asset.dto.AssetResponse;
import com.harsh.finance_project.asset.dto.CreateAssetRequest;
import com.harsh.finance_project.asset.dto.UpdateAssetRequest;
import com.harsh.finance_project.asset.model.Asset;
import com.harsh.finance_project.asset.service.AssetService;
import com.harsh.finance_project.common.dto.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assets")
public class AssetController {
    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping("")
    public ResponseEntity<AssetResponse> createAsset(@RequestBody @Valid CreateAssetRequest dto){
        Asset asset = assetService.createAsset(dto);

        return ResponseEntity.status(201).body(new AssetResponse(asset));
    }

    @GetMapping(value = "", params = "id")
    public ResponseEntity<AssetResponse> getAssetById(@RequestParam @NotNull Long id){
        Asset asset = assetService.getAssetById(id);
        return ResponseEntity.status(HttpStatus.OK).body(new AssetResponse(asset));
    }

    @GetMapping("")
    public ResponseEntity<PageResponse<AssetResponse>> getAllAssets(Pageable pageable){
        return ResponseEntity.status(HttpStatus.OK)
                .body(PageResponse.from(assetService.getAllAssets(pageable), AssetResponse::new));
    }

    @PutMapping("")
    public ResponseEntity<AssetResponse> updateAsset(@RequestParam @NotNull Long id, @RequestBody @Valid UpdateAssetRequest dto){
        Asset asset = assetService.updateAsset(id, dto);

        return ResponseEntity.status(HttpStatus.OK).body(new AssetResponse(asset));
    }

    @DeleteMapping("")
    public ResponseEntity<String> deleteAsset(@RequestParam @NotNull Long id){
        assetService.deleteAsset(id);

        return ResponseEntity.status(HttpStatus.OK).body("Asset Deleted Successfully");
    }
}
