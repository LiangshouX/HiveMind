package com.liangshou.adapter.controller;
import com.liangshou.common.Result;
import com.liangshou.infrastructure.datasource.po.OfficialPO;
import com.liangshou.service.OfficialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/officials")
@RequiredArgsConstructor
public class OfficialController {
    private final OfficialService officialService;

    @GetMapping
    public Result<List<OfficialPO>> listOfficials() {
        return Result.success(officialService.list());
    }

    @PostMapping
    public Result<Boolean> saveOfficial(@RequestBody OfficialPO official) {
        if (official.getId() == null) {
            official.setCreateTime(LocalDateTime.now());
        }
        official.setUpdateTime(LocalDateTime.now());
        return Result.success(officialService.saveOrUpdate(official));
    }
}
