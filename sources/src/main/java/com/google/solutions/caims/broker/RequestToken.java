//
// Copyright 2025 Google LLC
//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

package com.google.solutions.caims.broker;

import com.google.solutions.caims.workload.AttestationToken;
import org.jetbrains.annotations.NotNull;

/**
 * Token that entitles a client to perform a request to a specific workload instance.
 *
 * If we wanted to customize the lifetime of request tokens or implement quota charging,
 * we could implement the request token as a custom JWT that might contain the following:
 *
 * <ul>
 *   <li>An identifier for the workload instance (such as the instance name)</li>
 *   <li>A JWT ID to enforce one-time-use semantics</li>
 * </ul>
 *
 * To keep the implementation simple, we're not using a custom JWT here and instead
 * use tha attestation token as request token:
 *
 * <ul>
 *   <li>The attestation token is also a JWT, and we can verify it</li>
 *   <li>The client needs to "see" the full attestation token anyway so that it can convince
 *   itself of the integrity of the workload, so we can just as well use it for this purpose too.
 *   </li>
 * </ul>>
 *
 * @param attestationToken attestation token of the workload instance.
 */
public record RequestToken(
  @NotNull AttestationToken attestationToken
  ) {

  public RequestToken(@NotNull String token) {
    this(new AttestationToken(token));
  }
}
