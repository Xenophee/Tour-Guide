package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.dto.NearbyAttractionDTO;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;
import gpsUtil.GpsUtil;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tripPricer.Provider;
import tripPricer.TripPricer;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class TourGuideService {
	private final Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;

	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		
		Locale.setDefault(Locale.US);

		if (testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public VisitedLocation getUserLocation(User user) {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ? user.getLastVisitedLocation()
				: trackUserLocation(user).join();
		return visitedLocation;
	}


	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}

	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	/**
	 * Récupère les offres de voyage pour un utilisateur donné.
	 *
	 * @param user L'utilisateur pour lequel obtenir les offres de voyage.
	 * @return Une liste de fournisseurs proposant des offres de voyage pour l'utilisateur.
	 */
	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}


	/**
	 * Suit la localisation d'un utilisateur de manière asynchrone.
	 *
	 * @param user L'utilisateur dont la localisation doit être suivie.
	 * @return Un CompletableFuture contenant la localisation visitée de l'utilisateur.
	 */
	public CompletableFuture<VisitedLocation> trackUserLocation(User user) {
		// Obtenir la localisation de l'utilisateur de manière asynchrone
		CompletableFuture<VisitedLocation> locationFuture = CompletableFuture.supplyAsync(() ->
				gpsUtil.getUserLocation(user.getUserId())
		);

		// Calcul des récompenses de manière asynchrone après avoir obtenu la localisation
		return locationFuture.thenApplyAsync(visitedLocation -> {
			user.addToVisitedLocations(visitedLocation); // Mise à jour des lieux visités
			rewardsService.calculateRewards(user); // Calcul des récompenses (potentiellement lourd)
			return visitedLocation;
		});
	}


	/**
	 * Récupère les attractions les plus proches d'une localisation visitée.
	 *
	 * @param visitedLocation La localisation visitée par l'utilisateur.
	 * @param user L'utilisateur pour lequel trouver les attractions proches.
	 * @return Une liste des attractions les plus proches sous forme de DTO.
	 */
	public List<NearbyAttractionDTO> getNearByAttractions(VisitedLocation visitedLocation, User user) {
		List<CompletableFuture<NearbyAttractionDTO>> futures = gpsUtil.getAttractions().parallelStream()
				.sorted((a1, a2) -> Double.compare(
						rewardsService.getDistance(visitedLocation.location, a1),
						rewardsService.getDistance(visitedLocation.location, a2)))
				.limit(5)
				.map(attraction -> CompletableFuture.supplyAsync(() -> new NearbyAttractionDTO(
						attraction.attractionName,
						attraction.latitude,
						attraction.longitude,
						visitedLocation.location.latitude,
						visitedLocation.location.longitude,
						rewardsService.getDistance(visitedLocation.location, attraction),
						rewardsService.getRewardPoints(attraction, user)
				)))
				.toList();

		return futures.parallelStream()
				.map(CompletableFuture::join)
				.collect(Collectors.toList());
	}



	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				tracker.stopTracking();
			}
		});
	}

	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes
	// internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
					new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

}
