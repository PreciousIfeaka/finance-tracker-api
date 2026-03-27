package com.precious.finance_tracker.types;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUrls {
    private String url;

    private String mimeType;
}
