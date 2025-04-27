SATYAM || 2022462 || https://github.com/Satyam22462/WiFi_RSS_Logger_App.git

							=====================
							 WiFi RSS Logger App
							=====================

This Android app scans nearby Wi-Fi networks and logs their Received Signal Strength Indicator (RSSI) values. 
It allows you to associate Wi-Fi scan data with different physical locations (e.g., "Location 1", "Location 2", "Location 3") 
and saves the information in a local database. You can view saved data through visualizations such as bar charts and line graphs.

Main Features:
- Scan Wi-Fi networks and capture RSSI values.
- Associate each scan with a selected location.
- Save the top 100 RSSI readings (padded if fewer) into a local Room database.
- Visualize:
    - Average RSSI across locations (Bar Chart).
    - Individual Wi-Fi signal strengths over time (Line Chart).
- Clear all saved data when needed.

Screens:
1. Scan Screen
   - Select a location.
   - Start a Wi-Fi scan.
   - Save the scan results.
   - Navigate to view saved data.

2. Saved Data Screen
   - View average RSSI comparison across locations.
   - Detailed RSSI values per Wi-Fi network.
   - Delete all saved data.

Permissions Required:
- ACCESS_FINE_LOCATION (Needed to scan Wi-Fi networks)

Libraries Used:
- Jetpack Compose (UI)
- Room Database (Local data storage)
- MPAndroidChart (Bar and Line charts)
- AndroidX Navigation

Setup Instructions:
1. Clone this repository.
2. Open the project in Android Studio.
3. Ensure all dependencies are installed.
4. Run the app on a device or emulator with Wi-Fi capabilities.
5. Grant Location Permission when prompted.

Notes:
- Ensure Wi-Fi is turned on during scanning.
- The app saves up to 100 Wi-Fi readings per scan to maintain consistency.


