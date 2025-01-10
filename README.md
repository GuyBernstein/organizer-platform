# WhatsApp Content Manager
An AI-powered application that helps you organize and manage content you've sent to yourself on WhatsApp. Never lose track of important links, images, or ideas again!

## Watch my Video: https://www.youtube.com/watch?v=4JXdIhcAUog

## Features

- 🤖 Advanced AI categorization
- 📂 Smart message organization and categorization
- 🏷️ Intelligent tagging system
- 🔍 Advanced search capabilities
- 💾 Important conversations backup
- 🔒 Privacy-focused design

## Getting Started

- Docker and Docker Compose installed
- WhatsApp Business API credentials
- Anthropic API key
- Google Cloud Platform account with:
  - Storage bucket created
  - Service account with appropriate permissions
  - OAuth 2.0 client credentials

## Configuration

### 1. Application Properties

Edit `application.properties` and update the following values:

```properties
# WhatsApp configuration
whatsapp.api.token=yourwhatsapptoken

# Google Cloud Properties
gcp.bucket-name=your-bucket
gcp.project-id=your-project-id

# AI Configuration
anthropic.api.key=your-anthropic-key
```
### 2. Meta App Configuration

1. Go to the Meta Developer Portal (https://developers.facebook.com/)
2. Navigate to your WhatsApp Business App
3. In the App Dashboard, find the "Webhooks" section
4. Configure your Webhook:
   - Callback URL: Your application's webhook URL (must be HTTPS)
   - Verify Token: Set this to match the token in your application (`putYourToken` or your custom token)
5. Update Your WhatsApp Business Number:
   - Locate your WhatsApp Business phone number in the Meta Developer Portal
   - Open `src/main/resources/templates/index.html`
   - Replace `your-number-here` with your WhatsApp Business number in international format:
     ```html
     <!-- QR Code URL -->
     th:src="@{'https://api.qrserver.com/v1/create-qr-code/?size=200x200&amp;data=https://wa.me/1234567890'}"
     
     <!-- Direct link -->
     th:href="@{'https://wa.me/1234567890'}"
     ```
   - Make sure to include the country code without any symbols (e.g., 972501234567)

⚠️ **Important Notes:**
- Your webhook endpoint MUST be accessible via HTTPS
- Use a secure reverse proxy (like Nginx) or a service like ngrok for local development
- Keep your verify token secure and don't share it publicly
- The WhatsApp Business number must be verified and active in your Meta Business Account

### 3. Google Cloud Service Account

Create gcp/service-account-key.json with your GCP service account credentials:
```json
{
  "type": "your-service-type",
  "project_id": "your-project-id",
  "private_key_id": "your-key-id",
  "private_key": "-----BEGIN PRIVATE KEY-----\nyour-private-key\n-----END PRIVATE KEY-----\n",
  "client_email": "your-client-email",
  "client_id": "your-client-id",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "your-client-cert-url",
  "universe_domain": "googleapis.com"
}
```

### 3. OAuth Configuration

Create oauth/client-secret.json with your Google OAuth credentials:

```json
{
  "web": {
    "client_id": "your-client-id",
    "project_id": "your-project-id",
    "auth_uri": "https://accounts.google.com/o/oauth2/auth",
    "token_uri": "https://oauth2.googleapis.com/token",
    "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
    "client_secret": "your-client-secret",
    "redirect_uris": [
      "http://localhost:8080/login/oauth2/code/google"
    ]
  }
}
```

### 4. Admin Configuration

Update the admin details in UserService.java:

```java
public static final String ADMIN_EMAIL = "your@gmail.com";
private static final String ADMIN_WHATSAPP = "9725-yourphone";
private static final String ADMIN_PICTURE = "url-to-picture";
private static final String ADMIN_NAME = "your-name";
```

### Installation

1. Clone the repository:
```bash
git clone [your-repository-url]
cd [project-directory]
```

2. Start the Database:
```bash
docker-compose up -d
```

3. The application should now be running on `http://localhost:8080`

To stop the application:
```bash
docker-compose down
```


## Security & Privacy

```
- All user data is stored locally in PostgreSQL (in ./postgresdata directory)
- No access to other users' content
- End-to-end encryption for data transfer
- Regular security updates
- Secure credential management
```


## Contributing

1. Fork the repository
2. Create your feature branch:
```bash
git checkout -b feature/AmazingFeature
```
3. Commit your changes:
```bash
git commit -m 'Add some AmazingFeature'
```
4. Push to the branch:
```bash
git push origin feature/AmazingFeature
```
5. Open a Pull Request

## Support

For technical support or questions, please:
```
- Create an issue in this repository
- Contact the development team at guyu669@gmail.com
```


---
```
Made with ❤️ in Israel From Guy Bernstein
```
