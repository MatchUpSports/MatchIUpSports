package com.matchUpSports.boundedContext.futsalField.service;

import com.matchUpSports.base.exception.handler.DataNotFoundException;
import com.matchUpSports.boundedContext.futsalField.dto.FutsalFieldModifyDto;
import com.matchUpSports.boundedContext.futsalField.dto.FutsalFieldRegistrationDto;
import com.matchUpSports.boundedContext.futsalField.entity.FutsalField;
import com.matchUpSports.boundedContext.futsalField.form.CreateFutsalFieldForm;
import com.matchUpSports.boundedContext.futsalField.repository.FutsalFieldImageRepository;
import com.matchUpSports.boundedContext.futsalField.repository.FutsalFieldRepository;
import com.matchUpSports.boundedContext.member.entity.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FutsalFieldService {
    private final FutsalFieldRepository futsalFieldRepository;
    private final FutsalFieldImageService fieldImageService;
    private final FutsalFieldImageRepository fieldImageRepository;

    @Transactional(readOnly = true)
    public List<FutsalField> getFutsalFieldsOfCurrentUser(Member member) {
        return futsalFieldRepository.findByMember(member);
    }

    public FutsalField findByIdAndDeleteDateIsNull(Long id) {
        return futsalFieldRepository.findByIdAndDeleteDateIsNull(id)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 시설입니다."));
    }

    public FutsalField create(Member member, FutsalFieldRegistrationDto dto) {
        FutsalField futsalField = FutsalField.builder()
                .member(member)
                .fieldName(dto.getName())
                .fieldLocation(dto.getLocation())
                .price(dto.getPrice())
                .openTime(dto.getOpenTime())
                .closeTime(dto.getCloseTime())
                .courtCount(dto.getCourtCount())
                .registNum(dto.getRegistNum())
                .build();
        return futsalFieldRepository.save(futsalField);
    }

    //해당 지역에 있는 스타디움 찾는 로직
    public List<FutsalField> findFieldsByLocation(String location) {
        if (location == null || location.isEmpty()) {
            return futsalFieldRepository.findAll();
        }
        return futsalFieldRepository.findByFieldLocation(location);
    }


    @Transactional
    public FutsalField modify(FutsalField futsalField, FutsalFieldModifyDto dto) {
        futsalField.setFieldName(dto.getName());
        futsalField.setFieldLocation(dto.getLocation());
        futsalField.setPrice(dto.getPrice());
        futsalField.setOpenTime(dto.getOpenTime());
        futsalField.setCloseTime(dto.getCloseTime());
        futsalField.setCourtCount(dto.getCourtCount());

        return futsalFieldRepository.save(futsalField);
    }

    // soft-delete
    @Transactional
    public void delete(FutsalField futsalField) {
        futsalField.setDeleteDate(LocalDateTime.now());
        futsalFieldRepository.save(futsalField);
    }

    // hard-delete
    public void deleteHard(FutsalField futsalField) {
        futsalFieldRepository.delete(futsalField);
    }
}
//    @Transactional
//    public void create(@Valid CreateFutsalFieldForm createForm, Member member) {
//        try {
//            FutsalField field = FutsalField.builder()
//                    .member(member)
//                    .fieldName(createForm.getName())
//                    .fieldLocation(createForm.getLocation())
//                    .openTime(createForm.getOpenTime())
//                    .closeTime(createForm.getCloseTime())
//                    .courtCount(createForm.getCount())
//                    .price(createForm.getPrice())
//                    .registNum(createForm.getRegistNum())
//                    .build();
//
//            futsalFieldRepository.save(field);
//
//            List<MultipartFile> images = createForm.getImages();
//
//            for (MultipartFile image : images) {
//                if (image.isEmpty()) {
//                    log.info("이미지 뺴고 성공~!");
//                }
//            }
//
//            fieldImageService.uploadImages(field.getId(), images);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}

