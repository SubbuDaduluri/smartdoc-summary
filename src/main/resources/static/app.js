document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('uploadForm');
    const fileInput = document.getElementById('fileInput');
    const submitBtn = document.getElementById('submitBtn');
    const loading = document.getElementById('loading');
    const result = document.getElementById('result');
    const summaryText = document.getElementById('summaryText');
    const error = document.getElementById('error');
    const errorText = document.getElementById('errorText');

    form.addEventListener('submit', function(e) {
        e.preventDefault();

        const file = fileInput.files[0];
        if (!file) {
            showError('Please select a file.');
            return;
        }

        // Validate file type
        const allowedTypes = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
        if (!allowedTypes.includes(file.type)) {
            showError('Please select a PDF, DOC, or DOCX file.');
            return;
        }

        // Show loading
        hideAll();
        loading.classList.remove('hidden');
        submitBtn.disabled = true;

        // Create FormData
        const formData = new FormData();
        formData.append('file', file);

        // Send request
        fetch('/api/process/file', {
            method: 'POST',
            body: formData
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            hideAll();
            summaryText.textContent = data.processedText || 'Summary not available.';
            result.classList.remove('hidden');
        })
        .catch(err => {
            hideAll();
            showError('An error occurred while processing the file: ' + err.message);
        })
        .finally(() => {
            submitBtn.disabled = false;
        });
    });

    function hideAll() {
        loading.classList.add('hidden');
        result.classList.add('hidden');
        error.classList.add('hidden');
    }

    function showError(message) {
        errorText.textContent = message;
        error.classList.remove('hidden');
    }
});
