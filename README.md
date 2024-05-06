# PyFixer

PyFixer is an AI-powered CLI tool designed to streamline fixing common syntax and runtime issues within Python files. To do this it analyzes Python files by compiling them to check for any errors, then uses Perplexity.ai to fix these.

## Usage
Pre-Requisite: Will need a Perplexity.ai API key to use the tool.
```sh
./pyfixer.sh                             # On first run will build the gradle distribution
                                         # Subsequent runs will an start interactive session
./pyfixer.sh [path to python file]       # Will start analyzing and attempt to fix this file

./pyfixer.sh --rebuild                   # Included if gradle application needs to be rebuilt
```

## Demo

https://github.com/SushantPulavarthi/PyFixer/assets/69082740/a57463b6-a505-4d5d-91a6-9299ff58e0a1

https://github.com/SushantPulavarthi/PyFixer/assets/69082740/190a843b-9ce6-44fb-a4e8-a4f19a15695a

https://github.com/SushantPulavarthi/PyFixer/assets/69082740/69706181-687e-470f-a197-643c5fe7b587

[Alternative Link to Demos (Imgur)](https://imgur.com/a/9W7lrza)

## Environment Variables
Must define a `.env` file in running directory or define environment variables as System variables.
- `PPLX_API_KEY`
  - Perplexity.ai API Key that will be used during by the CLI tool.

Optional:
- `MAX_ALLOWED_ATTEMPTS`
  - Optional parameter that defines the maximum number requests the CLI tool will try before giving up.
  - By default, this is set to `5`.

