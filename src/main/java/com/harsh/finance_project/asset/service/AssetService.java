package com.harsh.finance_project.asset.service;

import com.harsh.finance_project.asset.dto.CreateAssetRequest;
import com.harsh.finance_project.asset.dto.UpdateAssetRequest;
import com.harsh.finance_project.asset.model.Asset;
import com.harsh.finance_project.asset.model.AssetStatus;
import com.harsh.finance_project.asset.repository.AssetRepository;
import jakarta.persistence.NoResultException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AssetService {
    private final AssetRepository repository;

    public AssetService(AssetRepository repository){
        this.repository = repository;
    }

    public Asset createAsset(CreateAssetRequest dto){
        LocalDateTime now = LocalDateTime.now();
        Asset asset = new Asset(dto.getName(),
                dto.getSymbol(),
                dto.getUnit(),
                AssetStatus.INACTIVE,
                dto.getAssetType(),
                dto.getCurrentPrice(),
                now,
                now);

        repository.save(asset);

        return asset;
    }

    public Asset getAssetById(Long id) {
        Optional<Asset> asset = repository.findById(id);

        return asset.orElseThrow(()-> new NoResultException());
    }

    public List<Asset> getAllAssets(){
        return repository.findAll();
    }

    public Asset updateAsset(Long id, UpdateAssetRequest dto){
        Asset asset = repository.findById(id).orElseThrow(()-> new NoResultException());

        if(dto.getName() != null){
            asset.setName(dto.getName());
        }
        if(dto.getSymbol() != null){
            asset.setSymbol(dto.getSymbol());
        }
        if(dto.getUnit() != null){
            asset.setUnit(dto.getUnit());
        }
        if(dto.getAssetType() != null){
            asset.setAssetType(dto.getAssetType());
        }
        if(dto.getCurrentPrice() != null){
            asset.setCurrentPrice(dto.getCurrentPrice());
        }
        asset.setUpdatedAt(LocalDateTime.now());

        repository.save(asset);

        return asset;
    }

    public void deleteAsset(Long id){
        Asset asset = repository.findById(id).orElseThrow(()-> new NoResultException());

        repository.delete(asset);
    }
}
