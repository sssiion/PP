package com.example.pp.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class WebClientFactory {
    private final WebClient.Builder builder;
    private final String tourBase;
    private final String busStopBase;
    private final String arrivalBase;
    private final String routeBase;
    private final String busLocBase;
    private final String subwayBase;
    private final String trainBase;
    private final String seoulMetroBase;

    public WebClientFactory(WebClient.Builder builder,
                            @Value("${tour.api.base-url}") String tourBase,
                            @Value("${tago.busstop.base-url}") String busStopBase,
                            @Value("${tago.arrival.base-url}") String arrivalBase,
                            @Value("${tago.route.base-url}") String routeBase,
                            @Value("${tago.busloc.base-url}") String busLocBase,
                            @Value("${tago.subway.base-url}") String subwayBase,
                            @Value("${tago.train.base-url}") String trainBase,
                            @Value("${seoulmetro.api.base-url}") String seoulMetroBase) {
        this.builder = builder;
        this.tourBase = tourBase; this.busStopBase = busStopBase; this.arrivalBase = arrivalBase;
        this.routeBase = routeBase; this.busLocBase = busLocBase; this.subwayBase = subwayBase; this.trainBase = trainBase;
        this.seoulMetroBase = seoulMetroBase;
    }

    public WebClient tour()          { return builder.clone().baseUrl(tourBase).build(); }
    public WebClient tagoBusStop()   { return builder.clone().baseUrl(busStopBase).build(); }
    public WebClient tagoArrival()   { return builder.clone().baseUrl(arrivalBase).build(); }
    public WebClient tagoRoute()     { return builder.clone().baseUrl(routeBase).build(); }
    public WebClient tagoBusLoc()    { return builder.clone().baseUrl(busLocBase).build(); }
    public WebClient tagoSubway()    { return builder.clone().baseUrl(subwayBase).build(); }
    public WebClient tagoTrain()     { return builder.clone().baseUrl(trainBase).build(); }
    public WebClient seoulMetro()    { return builder.clone().baseUrl(seoulMetroBase).build(); }
}
