# Bitcoin Wallet Android App

A simple Bitcoin wallet application for Android for the Bitcoin (Signet) network.

## Features

- **View Balance**
- **Send Transactions**
- **Transaction History**


## Setup Instructions

1. **Clone the Repository**.
2. **Open in Android Studio**:  
   Launch Android Studio.  
   Click on **File > Open** and navigate to the cloned repository.
3. **Add Required Files**:  
   Place the following files in the `assets` folder of the project:
    - **private_key.txt**: Contains your private key in WIF format. Warning: This is sensitive information. Handle it securely and never share it publicly.
    - **addresses.txt**: Contains a list of your Bitcoin addresses, one per line.

### Path: `app/src/main/assets/`


## Usage

- **Viewing Balance**: Upon launching, the app will display your Bitcoin balance.
- **Sending Bitcoin**:
    - Click on the **Send** button.
    - Enter the amount (in satoshis) and the destination address.
    - Click **Send** to initiate the transaction.
- **Transaction History**: Scroll down on the main screen to view your transactions.

## Important Notes

- **Security**:
    - **Private Key**: The `private_key.txt` file contains your private key. Ensure it is stored securely and is not exposed to unauthorized individuals.
    - **Assets Folder**: Files in the `assets` folder are bundled with the app. Be cautious when distributing the app to avoid leaking sensitive information.
    - **Test Network**: This app uses the Bitcoin Signet network, which is a test network. Do not send real bitcoins to addresses generated or used by this app.

## Dependencies

- Kotlin Coroutines
- Jetpack Compose
- Hilt for Dependency Injection
- BitcoinJ Library

## License

This project is licensed under the MIT License.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request.
