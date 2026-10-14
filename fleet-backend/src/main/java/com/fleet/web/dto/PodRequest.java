package com.fleet.web.dto;

import com.fleet.domain.PodType;

public record PodRequest(PodType type, String photoUrl, String signature, String otp, String recipientName) {
}
