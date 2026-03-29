package com.codmer.turepulseai.service;

import com.codmer.turepulseai.model.BusinessImageGenerateRequest;
import com.codmer.turepulseai.model.BusinessImageGenerateResponse;

public interface BusinessImageService {
    BusinessImageGenerateResponse generate(BusinessImageGenerateRequest request);

    // Generate a new image using an existing base image plus updated prompt/context.
    BusinessImageGenerateResponse regenerate(BusinessImageGenerateRequest request);
}

