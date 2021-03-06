/**
 * The MIT License
 * Copyright (c) 2015 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ee.ria.xroad.signer.tokenmanager.module;

import static ee.ria.xroad.signer.tokenmanager.token.HardwareTokenUtil.moduleGetInstance;

import java.util.ArrayList;
import java.util.List;

import akka.actor.Props;
import ee.ria.xroad.signer.tokenmanager.token.HardwareToken;
import ee.ria.xroad.signer.tokenmanager.token.HardwareTokenType;
import ee.ria.xroad.signer.tokenmanager.token.TokenType;
import iaik.pkcs.pkcs11.wrapper.PKCS11Constants;
import iaik.pkcs.pkcs11.wrapper.PKCS11Exception;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Module worker for hardware tokens.
 */
@Slf4j
@RequiredArgsConstructor
public class HardwareModuleWorker extends AbstractModuleWorker {

    private final HardwareModuleType module;

    private iaik.pkcs.pkcs11.Module pkcs11Module;

    @Override
    protected void initializeModule() throws Exception {
        if (pkcs11Module != null) {
            return;
        }

        log.info("Initializing module '{}' (library: {})", module.getType(),
                module.getPkcs11LibraryPath());
        try {
            pkcs11Module = moduleGetInstance(module.getPkcs11LibraryPath());
            pkcs11Module.initialize(null);
        } catch (Throwable t) {
            // Note that we catch all serious errors here since we do not
            // want Signer to crash if the module could not be loaded for
            // some reason.
            throw new RuntimeException(t);
        }
    }

    @Override
    protected void deinitializeModule() throws Exception {
        if (pkcs11Module == null) {
            return;
        }

        log.info("Deinitializing module '{}' (library: {})", module.getType(),
                module.getPkcs11LibraryPath());

        pkcs11Module.finalize(null);
    }

    @Override
    protected List<TokenType> listTokens() throws Exception {
        log.trace("Listing tokens on module '{}'", module.getType());

        iaik.pkcs.pkcs11.Slot[] slots = pkcs11Module.getSlotList(
                iaik.pkcs.pkcs11.Module.SlotRequirement.TOKEN_PRESENT);
        if (slots.length == 0) {
            log.warn("Did not get any slots from module '{}'. "
                    + "Reinitializing module.", module.getType());
            // Error code doesn't really matter as long as it's PKCS11Exception
            throw new PKCS11Exception(PKCS11Constants.CKR_GENERAL_ERROR);
        }

        log.info("Module '{}' got {} slots", module.getType(), slots.length);

        List<TokenType> tokens = new ArrayList<>();

        for (int slotIndex = 0; slotIndex < slots.length; slotIndex++) {
            tokens.add(createToken(slots, slotIndex));
        }

        return tokens;
    }

    private TokenType createToken(iaik.pkcs.pkcs11.Slot[] slots, int slotIndex)
            throws Exception {
        iaik.pkcs.pkcs11.Slot slot = slots[slotIndex];

        iaik.pkcs.pkcs11.Token pkcs11Token = slot.getToken();
        iaik.pkcs.pkcs11.TokenInfo tokenInfo = pkcs11Token.getTokenInfo();

        TokenType token = new HardwareTokenType(
            module.getType(),
            pkcs11Token,
            module.isForceReadOnly() || tokenInfo.isWriteProtected(),
            slotIndex,
            tokenInfo.getSerialNumber().trim(),
            tokenInfo.getLabel().trim(), // PKCS11 gives us only 32 bytes.
            module.isPinVerificationPerSigning(),
            module.isBatchSingingEnabled()
        );

        log.info("Module '{}' slot #{} has token: {}",
                new Object[] {module.getType(), slotIndex, token});
        return token;
    }

    @Override
    protected Props props(ee.ria.xroad.signer.protocol.dto.TokenInfo tokenInfo,
            TokenType tokenType) {
        return Props.create(HardwareToken.class, tokenInfo, tokenType);
    }
}
