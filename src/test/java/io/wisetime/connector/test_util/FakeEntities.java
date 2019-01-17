/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.test_util;

import com.github.javafaker.Faker;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.wisetime.generated.connect.Tag;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeRow;
import io.wisetime.generated.connect.User;

import static java.lang.String.format;

/**
 * Generator of entities with random field values. Typically used to mock real data.
 *
 * @author shane.xie@practiceinsight.io
 */
public class FakeEntities {

  private static final Faker FAKER = new Faker();
  private static final String TAG_PATH = format("/%s/%s/", FAKER.lorem().word(), FAKER.lorem().word());

  public TimeGroup randomTimeGroup() {
    final List<TimeRow> timeRows = randomEntities(this::randomTimeRow, 1, 10);

    return new TimeGroup()
        .callerKey(FAKER.bothify("#?#?#?#?#?"))
        .groupId(UUID.randomUUID().toString())
        .description(FAKER.lorem().paragraph())
        .totalDurationSecs(timeRows.stream().mapToInt(TimeRow::getDurationSecs).sum())
        .groupName(FAKER.color().name())
        .tags(randomEntities(() -> randomTag(TAG_PATH), 1, 3))
        .user(randomUser())
        .timeRows(timeRows)
        .narrativeType(randomEnum(TimeGroup.NarrativeTypeEnum.class))
        .durationSplitStrategy(randomEnum(TimeGroup.DurationSplitStrategyEnum.class));
  }

  public Tag randomTag() {
    return randomTag(format("/%s/", FAKER.lorem().word()));
  }

  public Tag randomTag(final String path) {
    return new Tag()
        .path(path)
        .name(FAKER.letterify("??-") + FAKER.number().numberBetween(1000, 9999))
        .description(FAKER.lorem().characters(30, 200));
  }

  public User randomUser() {
    final String firstName = FAKER.name().firstName();
    final String lastName = FAKER.name().lastName();
    return new User()
        .name(firstName + " " + lastName)
        .email(FAKER.internet().emailAddress(firstName))
        .externalId(FAKER.internet().emailAddress(firstName + "." + lastName))
        .businessRole(FAKER.company().profession())
        .experienceWeightingPercent(FAKER.random().nextInt(0, 100));
  }

  public TimeRow randomTimeRow() {
    return new TimeRow()
        .activity(FAKER.lorem().characters(30, 100))
        .activityHour(2018110100 + FAKER.random().nextInt(1, 23))
        .durationSecs(FAKER.random().nextInt(120, 600))
        .submittedDate(Long.valueOf(FAKER.numerify("20180#1#1#5#2####")))
        .modifier(FAKER.lorem().word())
        .source(randomEnum(TimeRow.SourceEnum.class));
  }

  private <T> List<T> randomEntities(final Supplier<T> supplier, final int min, final int max) {
    return IntStream
        .range(0, FAKER.random().nextInt(min, max))
        .mapToObj(i -> supplier.get())
        .collect(Collectors.toList());
  }

  private static <T extends Enum<?>> T randomEnum(final Class<T> clazz) {
    final int index = FAKER.random().nextInt(clazz.getEnumConstants().length);
    return clazz.getEnumConstants()[index];
  }
}
