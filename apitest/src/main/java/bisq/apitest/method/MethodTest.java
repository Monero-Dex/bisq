/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.apitest.method;

import bisq.proto.grpc.GetBalanceRequest;
import bisq.proto.grpc.LockWalletRequest;
import bisq.proto.grpc.RemoveWalletPasswordRequest;
import bisq.proto.grpc.SetWalletPasswordRequest;
import bisq.proto.grpc.UnlockWalletRequest;



import bisq.apitest.ApiTestCase;

public class MethodTest extends ApiTestCase {

    // Convenience methods for building gRPC request objects

    protected final GetBalanceRequest createBalanceRequest() {
        return GetBalanceRequest.newBuilder().build();
    }

    protected final SetWalletPasswordRequest createSetWalletPasswordRequest(String password) {
        return SetWalletPasswordRequest.newBuilder().setPassword(password).build();
    }

    protected final SetWalletPasswordRequest createSetWalletPasswordRequest(String oldPassword, String newPassword) {
        return SetWalletPasswordRequest.newBuilder().setPassword(oldPassword).setNewPassword(newPassword).build();
    }

    protected final RemoveWalletPasswordRequest createRemoveWalletPasswordRequest(String password) {
        return RemoveWalletPasswordRequest.newBuilder().setPassword(password).build();
    }

    protected final UnlockWalletRequest createUnlockWalletRequest(String password, long timeout) {
        return UnlockWalletRequest.newBuilder().setPassword(password).setTimeout(timeout).build();
    }

    protected final LockWalletRequest createLockWalletRequest() {
        return LockWalletRequest.newBuilder().build();
    }

    // Convenience methods for calling frequently used & thoroughly tested gRPC services.

    protected final long getBalance() {
        return grpcStubs.walletsService.getBalance(createBalanceRequest()).getBalance();
    }

    protected final void unlockWallet(String password, long timeout) {
        grpcStubs.walletsService.unlockWallet(createUnlockWalletRequest(password, timeout));
    }

    protected final void lockWallet() {
        grpcStubs.walletsService.lockWallet(createLockWalletRequest());
    }
}
