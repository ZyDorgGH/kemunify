# Kemunify: Smart Waste Management Application ♻️

**Kemunify** is an Android mobile application prototype developed to improve the operational efficiency and data accuracy of **Bank Sampah Kemuning**, a waste bank in Binong, Indonesia. The app's core innovation is its integration of a **Convolutional Neural Network (CNN)** model to automatically identify recyclable waste, streamlining the sorting and data entry process for waste bank staff.

---

## 🌟 Key Features

### 1. **User Authentication & Profile**
The app uses **Google Sign-In** for secure and convenient user authentication. This eliminates the need for users to create and remember new credentials, providing a seamless onboarding experience. After a successful login, the user's basic information (name, email) is automatically integrated into their profile.

### 2. **Waste Data Management**
The main dashboard serves as the operational control center for waste bank staff.
* **Data Recap:** The dashboard displays a structured recap of all waste transactions.
* **Data Entry:** Staff can use a comprehensive form to enter data for each customer.
* **Administrative Tools:** The dashboard includes an action button to export data to Google Drive, log out, and delete all stored data.

---

### 3. **AI-Powered Waste Detection**
This is the app's flagship feature, using a **MobileNet CNN** model to intelligently identify waste.
* **Image Analysis:** The feature supports analyzing images from the gallery or performing real-time detection with the camera.
* **Visual Results:** The app uses **bounding boxes** to highlight detected objects and provide a classification label (e.g., "Botol Plastik" - Plastic Bottle, "Kaleng Aluminium" - Aluminum Can).
* **Educational Information:** Tapping a detected object's label displays a detailed information card, including a description and practical **"Cara Melakukan Sorting"** (How to Sort) guidance.

---

## 🚀 Technology Stack

* **Platform:** Android
* **Core Technology:** Convolutional Neural Network (CNN) with MobileNetV2
* **Model:** MobileNet
* **Authentication:** Google Sign-In
* **Data Export:** Integration with Google Drive

---

## 📈 Project Impact
The **Kemunify** prototype successfully combines manual administrative functionality with smart waste detection technology. It provides a holistic solution for Bank Sampah Kemuning by:
* **Increasing Efficiency:** Automating waste identification and streamlining data entry.
* **Improving Accuracy:** Ensuring data is recorded consistently.
* **Educating Users:** Providing on-the-spot information and sorting instructions.

This project proves the feasibility of implementing a CNN model on a mobile platform to serve as a practical tool for waste management in real-world environments.
