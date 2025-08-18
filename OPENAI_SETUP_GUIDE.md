# OpenAI API Setup Guide

## Why You Need to Add Payment

The error "insufficient_quota" means your OpenAI account doesn't have billing set up. OpenAI requires a payment method to use their API services.

## Step-by-Step Setup

### 1. Add Payment Method to OpenAI

1. Go to [OpenAI Platform](https://platform.openai.com/)
2. Sign in to your account
3. Click on your profile icon → "Billing"
4. Click "Add payment method"
5. Enter your credit card details
6. Add a small amount (e.g., $5-10) to start

### 2. Get Your API Key

1. Go to [API Keys](https://platform.openai.com/api-keys)
2. Click "Create new secret key"
3. Copy the generated key (starts with `sk-`)

### 3. Configure the Application

#### Option A: Environment Variable (Recommended)
```bash
export OPENAI_API_KEY=your_api_key_here
```

#### Option B: Application Properties
Edit `src/main/resources/application.properties`:
```properties
spring.ai.openai.api-key=your_api_key_here
```

#### Option C: Production Properties
Edit `src/main/resources/application-prod.properties`:
```properties
spring.ai.openai.api-key=your_api_key_here
```

### 4. Cost Estimation

**Typical costs for this application:**
- **Document Processing**: $0.01-0.05 per document
- **Question Answering**: $0.01-0.03 per question
- **Monthly Usage**: $5-20 for moderate usage

**Cost Breakdown:**
- Embeddings: $0.00002 per 1K tokens
- Chat Completions: $0.00015 per 1K input tokens

### 5. Test the Setup

1. Restart your application
2. Upload a document
3. Ask a question
4. Check that you get AI-powered responses instead of fallback responses

## Fallback Mode

If you don't want to pay for OpenAI right now, the application will work in fallback mode:
- Uses simple keyword matching
- No AI-generated responses
- Basic document search functionality

## Security Notes

- Never commit API keys to version control
- Use environment variables in production
- Monitor your usage to avoid unexpected charges
- Set up usage limits in OpenAI dashboard

## Troubleshooting

### "insufficient_quota" Error
- Add payment method to OpenAI account
- Check API key is correct
- Verify billing is active

### "invalid_api_key" Error
- Check API key format (should start with `sk-`)
- Ensure key is copied correctly
- Verify key is active in OpenAI dashboard

### High Costs
- Monitor usage in OpenAI dashboard
- Set up usage alerts
- Consider using cheaper models for development

## Alternative Solutions

### 1. Use Free Tier (Limited)
- OpenAI offers some free credits
- Limited to specific models
- Good for testing

### 2. Local Models
- Use local LLM models (requires more setup)
- No API costs
- Slower performance

### 3. Other AI Providers
- Anthropic Claude
- Google Gemini
- Azure OpenAI

## Next Steps

1. Add payment method to OpenAI
2. Get API key
3. Configure application
4. Test with a document
5. Enjoy AI-powered document Q&A!
