/*
 * Copyright contributors to Besu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.api.jsonrpc.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hyperledger.besu.ethereum.BlockProcessingResult;
import org.hyperledger.besu.ethereum.BlockValidator;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.hyperledger.besu.ethereum.api.query.BlockchainQueries;
import org.hyperledger.besu.ethereum.chain.MutableBlockchain;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSpec;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DebugReplayBlockTest {

  private static final long BLOCK_NUMBER = 5L;

  private final MutableBlockchain blockchain = mock(MutableBlockchain.class);
  private final BlockValidator blockValidator = mock(BlockValidator.class);
  private final BlockHeader header = mock(BlockHeader.class);
  private final Block block = mock(Block.class);
  private DebugReplayBlock method;

  @BeforeEach
  public void setUp() {
    final BlockchainQueries queries = mock(BlockchainQueries.class);
    final ProtocolContext protocolContext = mock(ProtocolContext.class);
    final ProtocolSchedule protocolSchedule = mock(ProtocolSchedule.class);
    final ProtocolSpec protocolSpec = mock(ProtocolSpec.class);
    when(queries.getBlockchain()).thenReturn(blockchain);
    when(protocolContext.getBlockchain()).thenReturn(blockchain);
    when(blockchain.getBlockByNumber(BLOCK_NUMBER)).thenReturn(Optional.of(block));
    when(block.getHeader()).thenReturn(header);
    when(protocolSchedule.getByBlockHeader(header)).thenReturn(protocolSpec);
    when(protocolSpec.getBlockValidator()).thenReturn(blockValidator);
    method = new DebugReplayBlock(queries, protocolContext, protocolSchedule);
  }

  @Test
  public void returnsErrorWhenWorldStateUnavailable() {
    givenValidationResult(BlockProcessingResult.worldStateUnavailable("no parent state"));

    final JsonRpcResponse response = method.response(request());

    assertThat(response).isInstanceOf(JsonRpcErrorResponse.class);
    assertThat(((JsonRpcErrorResponse) response).getErrorType())
        .isEqualTo(RpcErrorType.WORLD_STATE_UNAVAILABLE);
    verify(blockchain, never()).forwardToBlock(any());
  }

  @Test
  public void returnsErrorWhenBlockFailsToProcess() {
    givenValidationResult(BlockProcessingResult.FAILED);

    final JsonRpcResponse response = method.response(request());

    assertThat(response).isInstanceOf(JsonRpcErrorResponse.class);
    assertThat(((JsonRpcErrorResponse) response).getErrorType())
        .isEqualTo(RpcErrorType.INTERNAL_ERROR);
    verify(blockchain, never()).forwardToBlock(any());
  }

  private void givenValidationResult(final BlockProcessingResult result) {
    when(blockValidator.validateAndProcessBlock(
            any(), any(), any(), any(), any(), any(), anyBoolean()))
        .thenReturn(result);
  }

  private JsonRpcRequestContext request() {
    return new JsonRpcRequestContext(
        new JsonRpcRequest("2.0", "debug_replayBlock", new Object[] {"0x5"}));
  }
}
