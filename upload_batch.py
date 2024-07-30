import requests
import json
import os
from datetime import datetime

# Define the API endpoint and headers
url = 'http://localhost:8080/item/batch'
headers = {
    'Content-Type': 'application/json',
    'X-Auth-Token': 'your-secret-token'  # If you are using an API key, add it here
}

# Path to the JSON file
file_path = '/home/jsbilgi/cass-stac/qv-toa-2023-1M.json'

# Path to the progress file
progress_file_path = 'progress.log'

# Path to the failed IDs file
failed_ids_file_path = 'failed_ids.log'

# Path to the log file
log_file_path = 'process.log'

# Batch size
batch_size = 100

def log(message):
    with open(log_file_path, 'a') as log_file:
        log_file.write(f"{datetime.now()} - {message}\n")

def load_progress():
    if os.path.exists(progress_file_path):
        with open(progress_file_path, 'r') as progress_file:
            return int(progress_file.read().strip())
    return 0

def save_progress(line_number):
    with open(progress_file_path, 'w') as progress_file:
        progress_file.write(str(line_number))

def log_failed_id(line, reason):
    with open(failed_ids_file_path, 'a') as failed_ids_file:
        failed_ids_file.write(f"{datetime.now()} - {line} - Reason: {reason}\n")

# Read the file and send data in batches
batch = []
line_number = 0

try:
    log("Process started")
    line_number = load_progress()
    with open(file_path, 'r') as file:
        for current_line_number, line in enumerate(file):
            if current_line_number < line_number:
                continue
            line = line.strip()
            if line:
                batch.append(json.loads(line))
                if len(batch) >= batch_size:
                    try:
                        response = requests.post(url, headers=headers, data=json.dumps(batch))
                        if response.status_code == 200:
                            log(f'Successfully uploaded batch of size {len(batch)}')
                            save_progress(current_line_number + 1)
                        else:
                            log(f'Failed to upload batch, Status code: {response.status_code}, Response: {response.text}')
                            for item in batch:
                                log_failed_id(json.dumps(item), f"Status code: {response.status_code}, Response: {response.text}")
                    except Exception as e:
                        log(f'Error uploading batch: {str(e)}')
                        for item in batch:
                            log_failed_id(json.dumps(item), str(e))
                    batch = []
        
        # Send the remaining items in the last batch
        if batch:
            try:
                response = requests.post(url, headers=headers, data=json.dumps(batch))
                if response.status_code == 200:
                    log(f'Successfully uploaded final batch of size {len(batch)}')
                    save_progress(current_line_number + 1)
                else:
                    log(f'Failed to upload final batch, Status code: {response.status_code}, Response: {response.text}')
                    for item in batch:
                        log_failed_id(json.dumps(item), f"Status code: {response.status_code}, Response: {response.text}")
            except Exception as e:
                log(f'Error uploading final batch: {str(e)}')
                for item in batch:
                    log_failed_id(json.dumps(item), str(e))
except Exception as e:
    log(f'Error processing file: {str(e)}')
finally:
    log("Process ended")

