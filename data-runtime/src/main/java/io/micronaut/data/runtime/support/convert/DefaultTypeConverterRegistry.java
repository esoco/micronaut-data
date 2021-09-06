/*
 * Copyright 2017-2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.data.runtime.support.convert;

import io.micronaut.context.BeanLocator;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.io.service.SoftServiceLoader;
import io.micronaut.data.model.runtime.TypeConverterRegistry;
import io.micronaut.data.model.runtime.convert.TypeConverter;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link TypeConverterRegistry}.
 */
@Singleton
@Internal
final class DefaultTypeConverterRegistry implements TypeConverterRegistry {

    private final BeanLocator beanLocator;
    private final List<TypeConverterProvider> typeConverterTransformers;
    private final Map<Class, TypeConverter<Object, Object>> transformedTypeConverters = new ConcurrentHashMap<>();

    DefaultTypeConverterRegistry(BeanLocator beanLocator) {
        this.beanLocator = beanLocator;
        List<TypeConverterProvider> typeConverterTransformers = new ArrayList<>();
        SoftServiceLoader.load(TypeConverterProvider.class).collectAll(typeConverterTransformers);
        this.typeConverterTransformers = typeConverterTransformers;
    }

    @Override
    public TypeConverter<Object, Object> getConverter(Class<?> converterClass) {
        if (TypeConverter.class.isAssignableFrom(converterClass)) {
            return (TypeConverter<Object, Object>) beanLocator.getBean(converterClass);
        }
        return transformedTypeConverters.computeIfAbsent(converterClass, c -> {
            for (TypeConverterProvider transformer : typeConverterTransformers) {
                if (transformer.supports(converterClass)) {
                    return transformer.provide(beanLocator, converterClass);
                }
            }
            throw new IllegalStateException("Unknown converter type: " + converterClass);
        });
    }
}