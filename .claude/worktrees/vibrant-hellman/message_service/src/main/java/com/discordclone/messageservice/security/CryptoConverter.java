package com.discordclone.messageservice.security;

import com.discordclone.messageservice.config.SpringContext;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CryptoConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return SpringContext.getBean(MessageCryptoService.class).encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return SpringContext.getBean(MessageCryptoService.class).decrypt(dbData);
    }
}