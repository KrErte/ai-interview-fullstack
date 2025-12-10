package ee.kerrete.ainterview.interview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.interview.dto.InterviewSimulationRequest;
import ee.kerrete.ainterview.interview.dto.InterviewSimulationResponse;
import ee.kerrete.ainterview.interview.dto.QuestionAnswerDto;
import ee.kerrete.ainterview.model.CompanyProfile;
import ee.kerrete.ainterview.model.InterviewSession;
import ee.kerrete.ainterview.repository.CompanyProfileRepository;
import ee.kerrete.ainterview.repository.InterviewSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InterviewService {

}

