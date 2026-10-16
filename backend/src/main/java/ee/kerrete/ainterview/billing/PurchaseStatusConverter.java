package ee.kerrete.ainterview.billing;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PurchaseStatusConverter implements AttributeConverter<PurchaseStatus, String> {

    @Override
    public String convertToDatabaseColumn(PurchaseStatus status) {
        return status == null ? null : status.code();
    }

    @Override
    public PurchaseStatus convertToEntityAttribute(String code) {
        return code == null ? null : PurchaseStatus.fromCode(code);
    }
}
