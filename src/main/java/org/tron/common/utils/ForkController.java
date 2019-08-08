package org.tron.common.utils;

import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.Parameter.ForkBlockVersionConsts;
import org.tron.core.config.Parameter.ForkBlockVersionEnum;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;

@Slf4j(topic = "utils")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ForkController extends ForkUtils{

  @Getter
  private Manager manager;

  public void init(Manager manager) {
    this.manager = manager;
    super.init(manager.getDynamicPropertiesStore());
  }

  public synchronized void update(BlockCapsule blockCapsule) {
    List<ByteString> witnesses = manager.getWitnessController().getActiveWitnesses();
    ByteString witness = blockCapsule.getWitnessAddress();
    int slot = witnesses.indexOf(witness);
    if (slot < 0) {
      return;
    }

    int version = blockCapsule.getInstance().getBlockHeader().getRawData().getVersion();
    if (version < ForkBlockVersionConsts.ENERGY_LIMIT) {
      return;
    }

    downgrade(version, slot);

    byte[] stats = manager.getDynamicPropertiesStore().statsByVersion(version);
    if (check(stats)) {
      upgrade(version, stats.length);
      return;
    }

    if (Objects.isNull(stats) || stats.length != witnesses.size()) {
      stats = new byte[witnesses.size()];
    }

    stats[slot] = VERSION_UPGRADE;
    manager.getDynamicPropertiesStore().statsByVersion(version, stats);
    logger.info(
        "*******update hard fork:{}, witness size:{}, solt:{}, witness:{}, version:{}",
        Streams.zip(witnesses.stream(), Stream.of(ArrayUtils.toObject(stats)), Maps::immutableEntry)
            .map(e -> Maps
                .immutableEntry(Wallet.encode58Check(e.getKey().toByteArray()), e.getValue()))
            .map(e -> Maps
                .immutableEntry(StringUtils.substring(e.getKey(), e.getKey().length() - 4),
                    e.getValue()))
            .collect(Collectors.toList()),
        witnesses.size(),
        slot,
        Wallet.encode58Check(witness.toByteArray()),
        version);
  }

  public static ForkController instance() {
    return ForkController.ForkControllerEnum.INSTANCE.getInstance();
  }

  private static enum ForkControllerEnum {
    INSTANCE;

    private ForkController instance = new ForkController();

    private ForkControllerEnum() {
    }

    private ForkController getInstance() {
      return this.instance;
    }
  }
}
