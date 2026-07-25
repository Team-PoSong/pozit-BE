package com.pozit.pozitserver.tag.service;

import com.pozit.pozitserver.tag.dto.response.TagResponse;
import com.pozit.pozitserver.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;

    public List<TagResponse> getTags(){
        return tagRepository
                .findAll()
                .stream()
                .map(TagResponse::from)
                .toList();
    }
}
