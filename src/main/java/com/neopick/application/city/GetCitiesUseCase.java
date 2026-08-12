package com.neopick.application.city;

import com.neopick.adapter.persistence.repository.CityJpaRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GetCitiesUseCase {

    private final CityJpaRepository cityJpaRepository;

    public GetCitiesUseCase(CityJpaRepository cityJpaRepository) {
        this.cityJpaRepository = cityJpaRepository;
    }

    @Cacheable(value = "cities", key = "'all'", unless = "#result.isEmpty()")
    public List<Map<String, Object>> allCities() {
        return cityJpaRepository.findAll().stream()
                .map(c -> Map.<String, Object>of(
                        "code", c.getCode(),
                        "name", c.getName(),
                        "hot", c.isHot()
                )).toList();
    }

    @Cacheable(value = "cities", key = "'hot'", unless = "#result.isEmpty()")
    public List<Map<String, Object>> hotCities() {
        return cityJpaRepository.findByIsHotTrueOrderBySortOrderAsc().stream()
                .map(c -> Map.<String, Object>of(
                        "code", c.getCode(),
                        "name", c.getName()
                )).toList();
    }
}
