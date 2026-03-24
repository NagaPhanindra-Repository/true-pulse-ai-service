package com.codmer.turepulseai.service;

import com.codmer.turepulseai.model.BusinessImageGenerateRequest;
import com.codmer.turepulseai.model.BusinessImageGenerateResponse;

public interface BusinessImageService {
    BusinessImageGenerateResponse generate(BusinessImageGenerateRequest request);
}

