FROM python:3.13-slim

ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app app
COPY training training
COPY pipeline pipeline

# The trained model is NOT baked into the image: it is mounted at /app/models
# (train it with: docker compose run --rm ml-service python -m training.train --data /data)
RUN mkdir -p /app/models && useradd --system --uid 10001 app && chown -R 10001 /app
USER 10001

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
