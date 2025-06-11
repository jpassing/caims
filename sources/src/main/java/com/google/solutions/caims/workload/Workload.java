package com.google.solutions.caims.workload;

import com.google.common.base.Preconditions;
import com.google.solutions.caims.AbstractServer;
import com.google.solutions.caims.protocol.EncryptedMessage;
import com.google.solutions.caims.protocol.Message;
import com.google.solutions.caims.protocol.RequestEncryptionKeyPair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Workload server, intended to run in a Confidential Space Trusted Execution Environment (TEE).
 */
public class Workload extends AbstractServer {
  /** The server's key pair, used for en/decrypting messages */
  private final @NotNull RequestEncryptionKeyPair keyPair;
  
  public @NotNull RequestEncryptionKeyPair.PublicKey publicKey() {
    return this.keyPair.publicKey();
  }

  public Workload(
    int listenPort,
    int threadPoolSize
  ) throws GeneralSecurityException, IOException {
    super(listenPort, threadPoolSize);

    //
    // Generate a new key pair. The key pair remains valid for the lifetime
    // of the process. Clients use the pair's public key to encrypt their
    // requests.
    //
    this.keyPair = RequestEncryptionKeyPair.generate();

    //
    // Register HTTP endpoints.
    //
    this.mapPostEncrypted("/", request -> handleInferenceRequest(request));
  }

  private EncryptedMessage handleInferenceRequest(
    @NotNull EncryptedMessage encryptedRequest
  ) {
    System.out.println("[INFO] Handling inference request...");
    try {
      //
      // Decrypt the request using the server's private key.
      //
      var request = encryptedRequest.decrypt(this.keyPair.privateKey());
      Preconditions.checkNotNull(
        request.senderPublicKey(),
        "The client did not provide a public key");

      //
      // Handle the request, for example by forwarding the request to a vLLM server
      // that's running in the same container.
      //
      // As an example, we generate a static fake response and send that back to the client,
      // encrypted it using the client's key.
      //
      var response = new Message(
        String.format(
          "> %s\nThat's a good question.",
          request.toString()),
        null);
      return response.encrypt(request.senderPublicKey());
    }
    catch (GeneralSecurityException | IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
}
