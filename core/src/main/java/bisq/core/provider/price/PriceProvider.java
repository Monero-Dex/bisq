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

package bisq.core.provider.price;

import bisq.core.locale.CurrencyUtil;
import bisq.core.provider.HttpClientProvider;

import bisq.network.http.HttpClient;
import bisq.network.p2p.P2PService;

import bisq.common.app.Version;
import bisq.common.util.MathUtils;
import bisq.common.util.Tuple2;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PriceProvider extends HttpClientProvider {

    private boolean shutDownRequested;

    // Do not use Guice here as we might create multiple instances
    public PriceProvider(HttpClient httpClient, String baseUrl) {
        super(httpClient, baseUrl, false);
    }

    public Tuple2<Map<String, Long>, Map<String, MarketPrice>> getAll() throws IOException {
        if (shutDownRequested) {
            return new Tuple2<>(new HashMap<>(), new HashMap<>());
        }

        Map<String, MarketPrice> marketPriceMap = new HashMap<>();
        String hsVersion = "";
        if (P2PService.getMyNodeAddress() != null)
            hsVersion = P2PService.getMyNodeAddress().getHostName().length() > 22 ? ", HSv3" : ", HSv2";

        String json = httpClient.get("getAllMarketPrices", "User-Agent", "bisq/"
                + Version.VERSION + hsVersion);


        LinkedTreeMap<?, ?> map = new Gson().fromJson(json, LinkedTreeMap.class);
        Map<String, Long> tsMap = new HashMap<>();
        tsMap.put("btcAverageTs", ((Double) map.get("btcAverageTs")).longValue());
        tsMap.put("poloniexTs", ((Double) map.get("poloniexTs")).longValue());
        tsMap.put("coinmarketcapTs", ((Double) map.get("coinmarketcapTs")).longValue());

        // get btc per xmr price to convert all prices to xmr
        // TODO (woodser): currently using bisq price feed, switch?
        Double btcPerXmr = null;
        List<?> list = (ArrayList<?>) map.get("data");
        for (Object obj : list) {
            LinkedTreeMap<?, ?> treeMap = (LinkedTreeMap<?, ?>) obj;
            String currencyCode = (String) treeMap.get("currencyCode");
            if ("XMR".equalsIgnoreCase(currencyCode)) {
                btcPerXmr = (Double) treeMap.get("price");
                break;
            }
        }

        final double btcPerXmrFinal = btcPerXmr;
        list.forEach(obj -> {
            try {
                LinkedTreeMap<?, ?> treeMap = (LinkedTreeMap<?, ?>) obj;
                String currencyCode = (String) treeMap.get("currencyCode");
                double price = (Double) treeMap.get("price");
                // json uses double for our timestampSec long value...
                long timestampSec = MathUtils.doubleToLong((Double) treeMap.get("timestampSec"));

                // convert price from btc to xmr
                boolean isFiat = CurrencyUtil.isFiatCurrency(currencyCode);
                if (isFiat) price = price * btcPerXmrFinal;
                else price = price / btcPerXmrFinal;

                // TODO (woodser): remove xmr from list since base currency and add btc, test by doing btc/xmr trade

                marketPriceMap.put(currencyCode, new MarketPrice(currencyCode, price, timestampSec, true));
            } catch (Throwable t) {
                log.error(t.toString());
                t.printStackTrace();
            }

        });
        return new Tuple2<>(tsMap, marketPriceMap);
    }

    public String getBaseUrl() {
        return httpClient.getBaseUrl();
    }

    public void shutDown() {
        shutDownRequested = true;
        httpClient.shutDown();
    }
}
