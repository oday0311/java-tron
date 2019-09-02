package org.tron.core.services;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.tron.common.application.TronApplicationContext;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db.Manager;

@Slf4j
public class DelegationServiceTest {

  private DelegationService delegationService;
  private Manager manager;

  public DelegationServiceTest(TronApplicationContext context) {
    delegationService = context.getBean(DelegationService.class);
    manager = context.getBean(Manager.class);
  }

  private void testPay(int cycle) {
    delegationService.payStandbyWitness();
    Wallet.setAddressPreFixByte(Constant.ADD_PRE_FIX_BYTE_MAINNET);
    byte[] sr1 = Wallet.decodeFromBase58Check("THKJYuUmMKKARNf7s2VT51g5uPY6KEqnat");
    long value = manager.getDelegationStore().getReward(cycle, sr1);
    long tmp = 0;
    for (int i = 0; i < 27; i++) {
      tmp += 100000000 + i;
    }
    double d = (double) 16000000 / tmp;
    long expect = (long) (d * 100000026);
    Assert.assertEquals(expect, value);
    delegationService.payBlockReward(sr1, 32000000);
    expect += 32000000;
    value = manager.getDelegationStore().getReward(cycle, sr1);
    Assert.assertEquals(expect, value);
  }

  private void testWithdraw() {
    //init
    manager.getDynamicPropertiesStore().saveCurrentCycleNumber(1);
    testPay(1);
    byte[] sr1 = Wallet.decodeFromBase58Check("THKJYuUmMKKARNf7s2VT51g5uPY6KEqnat");
    AccountCapsule accountCapsule = manager.getAccountStore().get(sr1);
    byte[] sr27 = Wallet.decodeFromBase58Check("TLTDZBcPoJ8tZ6TTEeEqEvwYFk2wgotSfD");
    accountCapsule.addVotes(ByteString.copyFrom(sr27), 10000000);
    manager.getAccountStore().put(sr1, accountCapsule);
    //
    long value = delegationService.queryReward(sr1);
    long reward = (long) ((double) manager.getDelegationStore().getReward(0, sr27) / 100000000
        * 10000000);
    System.out.println("testWithdraw:" + value + ", reward:" + reward);
    Assert.assertEquals(reward, value);
    delegationService.withdrawReward(sr1, null);
    accountCapsule = manager.getAccountStore().get(sr1);
    long allowance = accountCapsule.getAllowance();
    System.out.println("withdrawReward:" + allowance);
    Assert.assertEquals(reward, allowance);
  }

  public void test() {
    manager.getDynamicPropertiesStore().saveChangeDelegation(1);
    testPay(0);
    testWithdraw();
  }
}