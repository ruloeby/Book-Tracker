FROM python:3.11-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# avoid python writing .pyc files and use unbuffered logs
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

# copy and install dependencies first
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# copy app code
COPY . .

# expose default port (for documentation)
EXPOSE 5001

# run app - use 0.0.0.0 to accept connections from other containers
CMD ["python", "-c", "from app import app; app.run(host='0.0.0.0', port=5001, debug=False)"]